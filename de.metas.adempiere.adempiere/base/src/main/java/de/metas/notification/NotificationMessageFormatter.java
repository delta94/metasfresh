package de.metas.notification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.adempiere.model.PlainContextAware;
import org.adempiere.model.RecordZoomWindowFinder;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.adempiere.util.lang.ITableRecordReference;
import org.adempiere.util.lang.impl.TableRecordReference;
import org.apache.ecs.StringElement;
import org.apache.ecs.xhtml.a;
import org.compiere.util.Env;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;

import de.metas.document.engine.IDocumentBL;
import de.metas.i18n.IMsgBL;
import de.metas.logging.LogManager;
import de.metas.ui.web.WebuiURLs;
import lombok.NonNull;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
 * %%
 * Copyright (C) 2018 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

final class NotificationMessageFormatter
{
	public static final NotificationMessageFormatter newInstance()
	{
		return new NotificationMessageFormatter();
	}

	public static final String createUrlWithTitle(@NonNull final String url, @NonNull final String title)
	{
		return url + URL_TITLE_SEPARATOR + title;
	}

	// Services
	private static final Logger logger = LogManager.getLogger(NotificationMessageFormatter.class);
	private final IMsgBL msgBL = Services.get(IMsgBL.class);
	private final WebuiURLs webuiURLs = WebuiURLs.newInstance();

	// Constants
	private static final String URL_TITLE_SEPARATOR = "><";
	private static final String MSG_BottomText = "de.metas.notification.email.BottomText";

	//
	// Params
	private boolean html;
	private String adLanguage;
	private final Map<ITableRecordReference, String> recordDisplayTexts = new HashMap<>();
	private final Map<ITableRecordReference, Integer> recordWindowId = new HashMap<>();
	private String bottomURL;

	//
	// State
	private String _adLanguageEffective; // lazy

	private NotificationMessageFormatter()
	{
	}

	public NotificationMessageFormatter html(final boolean html)
	{
		this.html = html;
		return this;
	}

	public NotificationMessageFormatter adLanguage(final String adLanguage)
	{
		this.adLanguage = Check.isEmpty(adLanguage, true) ? null : adLanguage;
		return this;
	}

	private final String getLanguage()
	{
		if (_adLanguageEffective == null)
		{
			_adLanguageEffective = findLanguage();
		}
		return _adLanguageEffective;
	}

	private final String findLanguage()
	{
		return adLanguage != null ? adLanguage : Env.getAD_Language();
	}

	public NotificationMessageFormatter recordDisplayText(@NonNull final ITableRecordReference record, @NonNull final String displayText)
	{
		recordDisplayTexts.put(record, displayText);
		return this;
	}

	public NotificationMessageFormatter recordWindowId(@NonNull final ITableRecordReference record, final int adWindowId)
	{
		Check.assumeGreaterThanZero(adWindowId, "adWindowId");
		recordWindowId.put(record, adWindowId);
		return this;
	}

	public String format(@NonNull final String adMessage, final List<Object> params)
	{
		final ReplaceAfterFormatCollector replaceAfterFormat = new ReplaceAfterFormatCollector();
		final List<Object> adMessageParams = convertMessageParamsToString(params, replaceAfterFormat);
		String result = msgBL.getTranslatableMsgText(adMessage, adMessageParams)
				.translate(getLanguage());

		if (html)
		{
			result = new StringElement(result).toString();
		}

		result = replaceAfterFormat.replaceAll(result);

		final String bottomText = getBottomText();
		if (!Check.isEmpty(bottomText, true))
		{
			result += "\n" + bottomText;
		}

		return result;
	}

	private List<Object> convertMessageParamsToString(final List<Object> params, final ReplaceAfterFormatCollector replaceAfterFormatCollector)
	{
		if (params == null || params.isEmpty())
		{
			return ImmutableList.of();
		}

		return params.stream()
				.map(param -> convertMessageParamToString(param, replaceAfterFormatCollector))
				.collect(Collectors.toList()); // NOTE: don't use ImmutableList because we might have null elements
	}

	private Object convertMessageParamToString(final Object param, final ReplaceAfterFormatCollector replaceAfterFormatCollector)
	{
		if (param == null)
		{
			return null;
		}

		final ITableRecordReference record = toTableRecordReferenceOrNull(param);
		if (record != null)
		{
			if (html)
			{
				final a recordLink = getRecordHtmlLink(record);
				if (recordLink == null)
				{
					return getRecordDisplayText(record);
				}
				else
				{
					return replaceAfterFormatCollector.addAndGetKey(recordLink.toString());
				}
			}
			else
			{
				return getRecordDisplayText(record);
			}
		}

		if (param instanceof String)
		{
			final a link = toLinkOrNull(param.toString());
			if (link == null)
			{
				return param;
			}
			else
			{
				return replaceAfterFormatCollector.addAndGetKey(link.toString());
			}
		}

		return param;
	}

	private static ITableRecordReference toTableRecordReferenceOrNull(final Object obj)
	{
		if (obj == null)
		{
			return null;
		}
		else if (obj instanceof ITableRecordReference)
		{
			return (ITableRecordReference)obj;
		}
		// Extract the TableRecordReference from Map.
		// Usually that's the case when the parameters were deserialized and the the TableRecordRefererence was deserialized as Map.
		else if (obj instanceof Map)
		{
			final Map<?, ?> map = (Map<?, ?>)obj;
			return TableRecordReference.ofMapOrNull(map);
		}
		else
		{
			return null;
		}
	}

	private String getRecordDisplayText(@NonNull final ITableRecordReference record)
	{
		return recordDisplayTexts.computeIfAbsent(record, this::computeRecordDisplayText);
	}

	private String computeRecordDisplayText(final ITableRecordReference record)
	{
		try
		{
			// NOTE: the only reason why we have the service here and not declared at the top
			// it's because the IDocumentBL implementation it's not in this project (but in de.metas.bussiness)
			// so all unit tests for this class will fail even if they won't use the "recordDisplayText" feature.
			final IDocumentBL documentBL = Services.get(IDocumentBL.class);

			final Object targetRecordModel = record.getModel(PlainContextAware.createUsingOutOfTransaction());
			final String documentNo = documentBL.getDocumentNo(targetRecordModel);
			return documentNo;
		}
		catch (final Exception e)
		{
			logger.info("Failed retrieving record for " + record, e);
		}

		return "#" + record.getRecord_ID();
	}

	private a getRecordHtmlLink(final ITableRecordReference record)
	{
		if (record == null)
		{
			return null;
		}

		final String url = getRecordUrl(record);
		if (url == null)
		{
			return null;
		}

		final String targetRecordDisplayText = getRecordDisplayText(record);
		if (Check.isEmpty(targetRecordDisplayText))
		{
			return null;
		}

		return new a(url, targetRecordDisplayText);
	}

	private String getRecordUrl(@NonNull final ITableRecordReference record)
	{
		final int targetWindowId = getRecordWindowId(record);
		if (targetWindowId <= 0)
		{
			return null;
		}

		return webuiURLs.getDocumentUrl(String.valueOf(targetWindowId), String.valueOf(record.getRecord_ID()));
	}

	private int getRecordWindowId(@NonNull final ITableRecordReference record)
	{
		return recordWindowId.computeIfAbsent(record, RecordZoomWindowFinder::findAD_Window_ID);
	}

	private static a toLinkOrNull(final String text)
	{
		if (text == null)
		{
			return null;
		}

		final String textNorm = text.trim();
		final String textLC = textNorm.toLowerCase();
		if (textLC.startsWith("http://") || textLC.startsWith("https://"))
		{
			final String href;
			final String title;
			final int idx = textNorm.lastIndexOf(URL_TITLE_SEPARATOR);
			if (idx > 0)
			{
				href = textNorm.substring(0, idx);
				title = textNorm.substring(idx + URL_TITLE_SEPARATOR.length());
			}
			else
			{
				href = textNorm;
				title = textNorm;
			}

			return new a(href, title);
		}
		else
		{
			return null;
		}
	}

	public NotificationMessageFormatter bottomUrl(final String bottomURL)
	{
		this.bottomURL = bottomURL;
		return this;
	}

	private String getBottomText()
	{
		if (Check.isEmpty(bottomURL, true))
		{
			return null;
		}

		final String bottomURLText;
		if (html)
		{
			bottomURLText = new a(bottomURL, bottomURL).toString();
		}
		else
		{
			bottomURLText = bottomURL;
		}

		String bottomText = msgBL.getTranslatableMsgText(MSG_BottomText, bottomURLText)
				.translate(getLanguage());

		return bottomText;
	}

	//
	//
	//
	//
	//

	private static final class ReplaceAfterFormatCollector
	{
		private final Map<String, String> map = new HashMap<>();

		public String addAndGetKey(@NonNull final String partToReplace)
		{
			final String key = "#{" + map.size() + 1 + "}";
			map.put(key, partToReplace);
			return key;
		}

		public String replaceAll(@NonNull final String string)
		{
			if (map.isEmpty())
			{
				return string;
			}

			String result = string;
			for (final Map.Entry<String, String> e : map.entrySet())
			{
				result = result.replace(e.getKey(), e.getValue());
			}

			return result;
		}
	}
}

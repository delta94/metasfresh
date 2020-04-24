package de.metas.vertical.healthcare_ch.forum_datenaustausch_ch.base;

import javax.annotation.Nullable;

import de.metas.util.Check;
import de.metas.util.lang.ExternalId;
import lombok.experimental.UtilityClass;

/*
 * #%L
 * vertical-healthcare_ch.forum_datenaustausch_ch.invoice_base
 * %%
 * Copyright (C) 2020 metas GmbH
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

@UtilityClass
public class HealthcareCHHelper
{
	public ExternalId createBPartnerExternalIdForPatient(@Nullable final String billerEAN, @Nullable final String patientSSN)
	{
		if (Check.isBlank(billerEAN) || Check.isBlank(patientSSN))
		{
			return null;
		}
		return ExternalId.of("org:EAN-" + billerEAN + "_bp:SSN-" + patientSSN);
	}
}

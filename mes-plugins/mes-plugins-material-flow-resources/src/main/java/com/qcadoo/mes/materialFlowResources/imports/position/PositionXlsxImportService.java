/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.4
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */
package com.qcadoo.mes.materialFlowResources.imports.position;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.basic.constants.PalletNumberFields;
import com.qcadoo.mes.basic.constants.ProductFields;
import com.qcadoo.mes.basic.imports.services.XlsxImportService;
import com.qcadoo.mes.materialFlowResources.constants.DocumentFields;
import com.qcadoo.mes.materialFlowResources.constants.LocationFieldsMFR;
import com.qcadoo.mes.materialFlowResources.constants.PositionFields;
import com.qcadoo.mes.materialFlowResources.constants.StorageLocationFields;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.NumberService;

@Service
public class PositionXlsxImportService extends XlsxImportService {

    private static final String L_QCADOO_VIEW_VALIDATE_FIELD_ERROR_MISSING = "qcadooView.validate.field.error.missing";

    private static final String L_QCADOO_VIEW_VALIDATE_FIELD_ERROR_CUSTOM = "qcadooView.validate.field.error.custom";

    @Autowired
    private NumberService numberService;

    @Override
    public Entity createEntity(final String pluginIdentifier, final String modelName) {
        Entity position = getDataDefinition(pluginIdentifier, modelName).create();

        return position;
    }

    @Override
    public boolean validateEntity(final Entity position, final DataDefinition positionDD) {
        Entity document = position.getBelongsToField(PositionFields.DOCUMENT);
        Entity product = position.getBelongsToField(PositionFields.PRODUCT);
        Entity locationTo = document.getBelongsToField(DocumentFields.LOCATION_TO);

        validateQuantitiesAndUnits(position, positionDD, product);
        validateRequiredFields(position, positionDD, locationTo);
        validateStorageLocation(position, positionDD, product, locationTo);
        validatePalletNumber(position, positionDD);

        return position.isValid();
    }

    private void validateQuantitiesAndUnits(final Entity position, final DataDefinition positionDD, final Entity product) {
        BigDecimal quantity = position.getDecimalField(PositionFields.QUANTITY);
        BigDecimal givenQuantity = position.getDecimalField(PositionFields.GIVEN_QUANTITY);
        BigDecimal conversion = position.getDecimalField(PositionFields.CONVERSION);

        if (Objects.nonNull(product)) {
            String unit = product.getStringField(ProductFields.UNIT);
            String additionalUnit = product.getStringField(ProductFields.ADDITIONAL_UNIT);

            boolean quantitiesNonNull = Objects.nonNull(givenQuantity) && Objects.nonNull(conversion);

            if (Objects.nonNull(additionalUnit)) {
                position.setField(PositionFields.GIVEN_UNIT, additionalUnit);

                if (Objects.isNull(givenQuantity)) {
                    position.addError(positionDD.getField(PositionFields.GIVEN_QUANTITY),
                            L_QCADOO_VIEW_VALIDATE_FIELD_ERROR_MISSING);
                }
                if (Objects.isNull(conversion)) {
                    position.addError(positionDD.getField(PositionFields.CONVERSION), L_QCADOO_VIEW_VALIDATE_FIELD_ERROR_MISSING);
                }

                if (quantitiesNonNull) {
                    BigDecimal multipliedQuantity = quantity.multiply(conversion, numberService.getMathContext());

                    if (!multipliedQuantity.equals(givenQuantity)) {
                        position.addError(positionDD.getField(PositionFields.GIVEN_QUANTITY),
                                L_QCADOO_VIEW_VALIDATE_FIELD_ERROR_CUSTOM);
                    }
                }
            } else {
                position.setField(PositionFields.GIVEN_QUANTITY, quantity);
                position.setField(PositionFields.GIVEN_UNIT, unit);
                position.setField(PositionFields.CONVERSION, BigDecimal.ONE);
            }
        }
    }

    private void validateRequiredFields(final Entity position, final DataDefinition positionDD, final Entity locationTo) {
        BigDecimal price = position.getDecimalField(PositionFields.PRICE);
        String batch = position.getStringField(PositionFields.BATCH);
        Date productionDate = position.getDateField(PositionFields.PRODUCTION_DATE);
        Date expirationDate = position.getDateField(PositionFields.EXPIRATION_DATE);

        if (Objects.nonNull(locationTo)) {
            boolean requirePrice = locationTo.getBooleanField(LocationFieldsMFR.REQUIRE_PRICE);
            boolean requireBatch = locationTo.getBooleanField(LocationFieldsMFR.REQUIRE_BATCH);
            boolean requireProductionDate = locationTo.getBooleanField(LocationFieldsMFR.REQUIRE_PRODUCTION_DATE);
            boolean requireExpirationDate = locationTo.getBooleanField(LocationFieldsMFR.REQUIRE_EXPIRATION_DATE);

            if (requirePrice && Objects.isNull(price)) {
                position.addError(positionDD.getField(PositionFields.PRICE), L_QCADOO_VIEW_VALIDATE_FIELD_ERROR_MISSING);
            }
            if (requireBatch && Objects.isNull(batch)) {
                position.addError(positionDD.getField(PositionFields.BATCH), L_QCADOO_VIEW_VALIDATE_FIELD_ERROR_MISSING);
            }
            if (requireProductionDate && Objects.isNull(productionDate)) {
                position.addError(positionDD.getField(PositionFields.PRODUCTION_DATE),
                        L_QCADOO_VIEW_VALIDATE_FIELD_ERROR_MISSING);
            }
            if (requireExpirationDate && Objects.isNull(expirationDate)) {
                position.addError(positionDD.getField(PositionFields.EXPIRATION_DATE),
                        L_QCADOO_VIEW_VALIDATE_FIELD_ERROR_MISSING);
            }
        }
    }

    private void validateStorageLocation(final Entity position, final DataDefinition positionDD, final Entity product,
            final Entity locationTo) {
        Entity storageLocation = position.getBelongsToField(PositionFields.STORAGE_LOCATION);

        if (Objects.nonNull(product) && Objects.nonNull(locationTo) && Objects.nonNull(storageLocation)) {
            Entity storageLocationLocation = storageLocation.getBelongsToField(StorageLocationFields.LOCATION);
            Entity storageLocationProduct = storageLocation.getBelongsToField(StorageLocationFields.PRODUCT);

            if (!storageLocation.isActive()
                    || (Objects.nonNull(storageLocationLocation) && !locationTo.equals(storageLocationLocation))
                    || (Objects.nonNull(storageLocationProduct) && !product.equals(storageLocationProduct)))
                position.addError(positionDD.getField(PositionFields.STORAGE_LOCATION),
                        L_QCADOO_VIEW_VALIDATE_FIELD_ERROR_CUSTOM);
        }
    }

    private void validatePalletNumber(final Entity position, final DataDefinition positionDD) {
        Entity palletNumber = position.getBelongsToField(PositionFields.PALLET_NUMBER);

        if (Objects.nonNull(palletNumber)) {
            Date issueDateTime = palletNumber.getDateField(PalletNumberFields.ISSUE_DATE_TIME);

            if (!palletNumber.isActive() || Objects.nonNull(issueDateTime)) {
                position.addError(positionDD.getField(PositionFields.PALLET_NUMBER), L_QCADOO_VIEW_VALIDATE_FIELD_ERROR_CUSTOM);
            }
        }
    }

}

package com.qcadoo.mes.core.view.elements.comboBox;

import java.util.Map;

public final class EntityComboBoxValue {

    private Long selectedValue;

    private Map<Long, String> values;

    public Long getSelectedValue() {
        return selectedValue;
    }

    public void setSelectedValue(final Long selectedValue) {
        this.selectedValue = selectedValue;
    }

    public Map<Long, String> getValues() {
        return values;
    }

    public void setValues(final Map<Long, String> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        if (selectedValue == null) {
            return null;
        }
        return selectedValue.toString();
    }

}

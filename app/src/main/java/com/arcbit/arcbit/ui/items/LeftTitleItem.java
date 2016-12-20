package com.arcbit.arcbit.ui.items;

public class LeftTitleItem implements Item {
    public final String title;

    public LeftTitleItem(String title) {
        this.title = title;
    }

    @Override
    public boolean isSection() {
        return false;
    }
}

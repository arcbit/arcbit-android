package com.arcbit.arcbit.ui.items;


public class EntryItem implements Item {
	public final String title;
	public final String subtitle;
	public Object accountObject;

	public EntryItem(String title, String subtitle, Object accountObject) {
		this.title = title;
		this.subtitle = subtitle;
		this.accountObject = accountObject;
	}

	public EntryItem(String title, String subtitle) {
		this.title = title;
		this.subtitle = subtitle;
	}
	
	@Override
	public boolean isSection() {
		return false;
	}
}

package com.github.bananaj.model.list;

/**
 * Whether a list is public or private.
 */
public enum ListVisibility {

	PUBLIC("pub"),
	PRIVATE("prv");
	
	private String stringRepresentation;
	
	ListVisibility(String stringRepresentation ) {
		setStringRepresentation(stringRepresentation);
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}

	/**
	 * @param stringRepresentation Set the stringRepresentation for the enum constant.
	 */
	private void setStringRepresentation(String stringRepresentation) {
		this.stringRepresentation = stringRepresentation;
	}

	/**
	 * Find the list visibility type based on the API value.
	 * @param value the value from the API (either {@code pub} or {@code prv}
	 * @return the appropriate visibilty value
	 * @throws IllegalArgumentException if the value is not recognized
	 */
	public static ListVisibility lookup(String value) {
		switch (value==null?"":value) {
			case "pub":
				return PUBLIC;
			case "prv":
				return PRIVATE;
			default:
				throw new IllegalArgumentException("Unknown visibility '" + value + "'");
		}
	}
}

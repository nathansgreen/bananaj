package com.github.bananaj.model.list.segment;

import org.json.JSONObject;

import com.github.bananaj.exceptions.ConditionException;

/**
 * Segment option condition condition_type uses a Number value
 */
public class DoubleCondition implements AbstractCondition {

	private ConditionType condition_type;
	private String field;
	private Operator operator;
	private Double value;

	/**
	 * Used when created a Condition locally with the Builder class
	 * @see Builder
	 * @param b
	 */

	public DoubleCondition(Builder b) throws ConditionException {
		if (b.condition_type == null) {
			throw new ConditionException("A condition need a condition_type.");
		}

		if (b.operator == null) {
			throw new ConditionException("A condition need an operator.");
		}

		if (b.field == null) {
			throw new ConditionException("A condition need a field to operate on.");
		}

		if (b.value == null) {
			throw new ConditionException("A condition need a value to compare on.");
		}

		this.condition_type = b.condition_type;
		this.operator = b.operator;
		this.field = b.field;
		this.value = b.value;
	}

	public ConditionType getConditionType() {
		return condition_type;
	}

	public String getField() {
		return field;
	}

	public Operator getOp() {
		return operator;
	}

	public Double getValue() {
		return value;
	}

	@Override
	public JSONObject getJsonRepresentation(){
		JSONObject condition = new JSONObject();
		condition.put("condition_type", getConditionType().toString());
		condition.put("op", getOp().toString());
		condition.put("field", getField());
		condition.put("value", getValue());

		return condition;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "    " + getJsonRepresentation().toString();
	}

	public static class Builder {
		private ConditionType condition_type;
		private String field;
		private Operator operator;
		private Double value;

		public Builder conditionType(ConditionType condition_type) {
			this.condition_type = condition_type;
			return this;
		}

		public Builder field(String field) {
			this.field = field;
			return this;
		}

		public Builder operator(Operator op) {
			this.operator = op;
			return this;
		}

		public Builder value(Double value) {
			this.value = value;
			return this;
		}

		public DoubleCondition build() throws ConditionException {
			return new DoubleCondition(this);
		}
	}

}

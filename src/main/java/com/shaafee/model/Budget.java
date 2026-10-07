package com.shaafee.model;

import java.time.YearMonth;

public class Budget {
    public YearMonth getMonth() {
        return month;
    }

    private YearMonth month;
    private String category;
    private double limit;

    public Budget(YearMonth month, String category, double limit) {
        this.month = month;
        this.category = category;
        this.limit = limit;

    }
    public Budget() {}

    public void setMonth(YearMonth month) {
        this.month = month;
    }

    @Override
    public String toString() {
        return "Month{" +
                "month=" + month +
                ", category='" + category + '\'' +
                ", limit=" + limit +
                '}';
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getLimit() {
        return limit;
    }

    public void setLimit(double limit) {
        this.limit = limit;
    }
}

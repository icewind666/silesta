package com.silesta.models;

import java.util.Date;

public class BankOperation implements Cloneable  {
    public Date getOpDate() {
        return opDate;
    }

    public void setOpDate(Date opDate) {
        this.opDate = opDate;
    }

    private Date opDate;

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryIdExternal() {
        return categoryIdExternal;
    }

    public void setCategoryIdExternal(String categoryIdExternal) {
        this.categoryIdExternal = categoryIdExternal;
    }

    private double amount;
    private String description;
    private String category;
    private String categoryIdExternal;

    public boolean isIncome() {
        return isIncome;
    }

    public void setIncome(boolean income) {
        isIncome = income;
    }

    private boolean isIncome;


    public BankOperation() {
    }

    public Object clone() throws
            CloneNotSupportedException
    {
        return super.clone();
    }
}

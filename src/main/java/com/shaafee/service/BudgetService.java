package com.shaafee.service;

import com.shaafee.model.Budget;
import com.shaafee.model.Transaction;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class BudgetService {
    private List<Budget> budgets=new ArrayList<>();


    // set budget
    public boolean setBudget(Budget budget) {
        for (int i = 0; i < budgets.size(); i++) {
            Budget existing = budgets.get(i);
            if (existing.getMonth().equals(budget.getMonth())
                    && existing.getCategory().equalsIgnoreCase(budget.getCategory())) {
                budgets.set(i, budget);   // same month + category: replace the old limit
                return true;
            }
        }
        budgets.add(budget);              // otherwise it's a new budget
        return true;
    }
    //get budget per month
    public Budget getBudget(YearMonth yearMonth){
        return budgets.stream().filter(b->b.getMonth().equals(yearMonth)).findFirst().get();
    }
    public List<Budget> getBudgets(){
        return budgets;
    }



}

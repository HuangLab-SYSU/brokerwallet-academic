package com.example.brokerfi.xc.PageManager;

import java.util.Stack;

public class PageManager {
    private static PageManager instance;
    private Stack<Class> pageStack = new Stack<>();

    private PageManager() {
    }

    public static PageManager getInstance() {
        if (instance == null) {
            instance = new PageManager();
        }
        return instance;
    }

    public void navigateTo(Class destinationPage) {

        pageStack.push(destinationPage);
    }

    public void navigateBackTo(Class destinationPage) {

        Stack<Class> tempStack = new Stack<>();
        Class currentPage;
        while (!pageStack.isEmpty()) {
            currentPage = pageStack.pop();
            if (currentPage == destinationPage) {
                break;
            }
            tempStack.push(currentPage);
        }

        while (!tempStack.isEmpty()) {
            pageStack.push(tempStack.pop());
        }
    }
}


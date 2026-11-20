package com.inventory.controller;

/** Sayfalama parametrelerini güvenli aralıkta tutar. */
final class Paging {

    static final int MAX_SIZE = 100;

    private Paging() {
    }

    static int page(int page) {
        return Math.max(page, 0);
    }

    static int size(int size) {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }
}

package de.feelix.sierra.utilities;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Pagination<T> {

    private final List<T> items;
    private final int     itemsPerPage;

    /**
     * The Pagination function takes a list of items and the number of items per page,
     * then returns a list of pages. Each page is itself a list containing the items for that page.

     *
     * @param items&lt;T&gt; items Store the items that are to be paginated
     * @param itemsPerPage itemsPerPage Set the number of items per page
     */
    public Pagination(List<T> items, int itemsPerPage) {
        this.items = items;
        this.itemsPerPage = itemsPerPage;
    }

    /**
     * The itemsForPage function returns a list of items for the given page.
     *
     * @param page page Determine the page number
     *
     * @return A list of items from the given page
     */
    public List<T> itemsForPage(int page) {
        if (page < 1) {
            throw new IllegalArgumentException("Parameter page must be greater than 1.");
        }
        if (this.items.isEmpty()) {
            return new ArrayList<>();
        }

        int startIndex = (page - 1) * this.itemsPerPage;
        int endIndex   = Math.min(startIndex + this.itemsPerPage, this.items.size());
        return this.items.subList(startIndex, endIndex);
    }

    /**
     * The totalPages function returns the total number of pages in the pagination.
     *
     * @return The number of pages for the items
     */
    public int totalPages() {
        return (int) Math.ceil((double) this.items.size() / this.itemsPerPage);
    }
}

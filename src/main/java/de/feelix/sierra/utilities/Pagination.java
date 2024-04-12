package de.feelix.sierra.utilities;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Pagination<T> {

    /**
     * The items variable is a private final List that stores the items to be paginated.
     * <p>
     * The items list is used by the Pagination class to perform pagination operations
     * such as retrieving a sublist of items for a given page number and calculating
     * the total number of pages.
     * <p>
     * This variable is only accessible within the Pagination class and cannot be modified
     * once it is assigned a value.
     */
    private final List<T> items;
    /**
     * The itemsPerPage variable represents the number of items to be displayed per page in a paginated list.
     * It is used by the Pagination class to determine the size of each page when retrieving a sublist of items.
     * <p>
     * The value of itemsPerPage should be a positive integer greater than zero.
     * <p>
     * Example usage:
     * <p>
     * Pagination<String> pagination = new Pagination<>(itemsList, 10);
     * List<String> pageItems = pagination.itemsForPage(1);
     * <p>
     * In the above example, the itemsPerPage variable is set to 10, which means each page will display 10 items.
     * The itemsForPage method is then called with the page number 1, which returns a sublist of items for that page.
     */
    private final int     itemsPerPage;


    /**
     * The Pagination class represents a utility for paginating a list of items.
     * It allows retrieving a sublist of items for a given page number and calculating the total number of pages.
     * <p>
     * Example usage:
     * <pre>{@code
     * List<String> itemsList = Arrays.asList("Item 1", "Item 2", "Item 3", "Item 4", "Item 5", "Item 6", "Item 7", "Item 8", "Item 9", "Item 10");
     * Pagination<String> pagination = new Pagination<>(itemsList, 5);
     * List<String> pageItems = pagination.itemsForPage(2);
     * }</pre>
     */
    public Pagination(List<T> items, int itemsPerPage) {
        this.items = items;
        this.itemsPerPage = itemsPerPage;
    }

    /**
     * Returns a sublist of items for the given page number.
     *
     * @param page The page number (starting from 1) for which to retrieve the items
     * @return A sublist of items for the given page number
     * @throws IllegalArgumentException if the page number is less than 1
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
     * Calculates the total number of pages required for pagination.
     *
     * @return The total number of pages
     */
    public int totalPages() {
        return (int) Math.ceil((double) this.items.size() / this.itemsPerPage);
    }
}

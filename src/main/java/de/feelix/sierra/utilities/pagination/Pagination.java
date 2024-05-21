package de.feelix.sierra.utilities.pagination;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * The Pagination class represents a utility for paginating a list of items.
 * It allows retrieving a sublist of items for a given page number and calculating the total number of pages.
 *
 * @param <T> The type of items in the Pagination
 */
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
    private final int itemsPerPage;

    /**
     * The constant ITEMS_PER_PAGE represents the number of items to be displayed per page in a pagination system.
     * It specifies the number of items that should be shown to the user at a time in a paginated list.
     * The value is set to 10.
     */
    private static final int ITEMS_PER_PAGE = 10;

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
     * Retrieves a sublist of items for the specified page number.
     *
     * @param requestedPage The page number for which to retrieve the items
     * @return The list of items for the specified page number
     */
    public List<T> itemsForPage(int requestedPage) {
        validatePageNumber(requestedPage);
        if (this.items.isEmpty()) {
            return new ArrayList<>();
        }
        int startIndex = calculateStartIndex(requestedPage);
        int endIndex   = calculateEndIndex(startIndex);
        return this.items.subList(startIndex, endIndex);
    }

    /**
     * Calculates the start index for pagination based on the given page number.
     *
     * @param page The page number for which to calculate the start index
     * @return The start index for pagination
     */
    private int calculateStartIndex(int page) {
        return (page - 1) * this.itemsPerPage;
    }

    /**
     * Calculates the end index for pagination based on the given start index.
     *
     * @param startIndex The start index for pagination
     * @return The end index for pagination
     */
    private int calculateEndIndex(int startIndex) {
        return Math.min(startIndex + this.itemsPerPage, this.items.size());
    }

    /**
     * Validates the given page number. Throws an exception if the page number is less than 1.
     *
     * @param page The page number to validate
     * @throws IllegalArgumentException If the page number is less than 1
     */
    private void validatePageNumber(int page) {
        if (page < 1) {
            throw new IllegalArgumentException("Parameter page must be greater than 1.");
        }
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

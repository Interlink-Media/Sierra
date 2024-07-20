package de.feelix.sierra.utilities.pagination;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Pagination<T> {

    private final List<T> entries;
    private final int itemsPerPage;

    /**
     * The Pagination class is used to implement pagination functionality for a given list of entries.
     * Pagination divides the list into smaller subsets called pages, each containing a specified number of items per
     * page.
     * This class allows for easy navigation between pages and retrieval of items based on the requested page number.
     *
     * @param entries The type of objects in the list of entries.
     */
    public Pagination(List<T> entries, int itemsPerPage) {
        this.entries = entries;
        this.itemsPerPage = itemsPerPage;
    }

    /**
     * Retrieves a list of items for the specified page number.
     *
     * @param requestedPage The requested page number.
     * @return A list of items for the requested page.
     */
    public List<T> itemsForPage(int requestedPage) {
        validatePageNumber(requestedPage);
        if (this.entries.isEmpty()) {
            return new ArrayList<>();
        }
        int startIndex = calculateStartIndex(requestedPage);
        int endIndex = calculateEndIndex(startIndex);
        return this.entries.subList(startIndex, endIndex);
    }

    /**
     * Calculates the start index of the items for the specified page number.
     *
     * @param page The requested page number.
     * @return The start index of the items for the requested page.
     * @throws IllegalArgumentException if page is less than 1.
     */
    private int calculateStartIndex(int page) {
        return (page - 1) * this.itemsPerPage;
    }

    /**
     * Calculates the end index of the items for the specified start index.
     *
     * @param startIndex The start index of the items.
     * @return The end index of the items.
     */
    private int calculateEndIndex(int startIndex) {
        return Math.min(startIndex + this.itemsPerPage, this.entries.size());
    }

    /**
     * Validates the page number parameter.
     *
     * @param page The page number to be validated.
     * @throws IllegalArgumentException if the page number is less than 1.
     */
    private void validatePageNumber(int page) {
        if (page < 1) {
            throw new IllegalArgumentException("Parameter page must be greater than 1.");
        }
    }

    /**
     * Calculates the total number of pages based on the current number of entries and items per page.
     *
     * @return The total number of pages.
     */
    public int totalPages() {
        return (int) Math.ceil((double) this.entries.size() / this.itemsPerPage);
    }
}

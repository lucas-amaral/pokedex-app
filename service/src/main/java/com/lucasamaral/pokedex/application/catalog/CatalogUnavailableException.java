package com.lucasamaral.pokedex.application.catalog;

/**
 * The external catalog could not answer: it is down, too slow or returned an error.
 */
public class CatalogUnavailableException extends RuntimeException {

    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

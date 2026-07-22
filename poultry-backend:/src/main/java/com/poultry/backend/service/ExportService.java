package com.poultry.backend.service;

import java.util.List;
import java.util.Map;

public interface ExportService {
    /**
     * Generic parameterizable method to export list content to PDF format.
     */
    byte[] exportToPdf(List<?> data, Map<String, Object> parameters);

    /**
     * Generic method to export rows to Microsoft Excel worksheet format.
     */
    byte[] exportToExcel(List<?> data, List<String> headers);

    /**
     * Generic method to export data rows to CSV plain text representation.
     */
    byte[] exportToCsv(List<?> data, List<String> headers);
}

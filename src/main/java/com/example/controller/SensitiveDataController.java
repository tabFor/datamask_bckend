package com.example.controller;

import com.example.model.SensitiveColumn;
import com.example.service.SensitiveDataDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sensitive-data")
public class SensitiveDataController {

    @Autowired
    private SensitiveDataDetector sensitiveDataDetector;

    @GetMapping("/detect/{tableName}")
    public ResponseEntity<List<SensitiveColumn>> detectTableSensitiveColumns(@PathVariable String tableName) {
        List<SensitiveColumn> sensitiveColumns = sensitiveDataDetector.detectSensitiveColumns(tableName);
        return ResponseEntity.ok(sensitiveColumns);
    }

    @GetMapping("/detect")
    public ResponseEntity<List<SensitiveColumn>> detectAllSensitiveColumns() {
        List<SensitiveColumn> sensitiveColumns = sensitiveDataDetector.detectAllSensitiveColumns();
        return ResponseEntity.ok(sensitiveColumns);
    }

    @PostMapping("/detect/column")
    public ResponseEntity<SensitiveColumn> detectColumn(@RequestBody Map<String, String> request) {
        String columnName = request.get("columnName");
        String dataType = request.get("dataType");
        
        if (columnName == null || dataType == null) {
            return ResponseEntity.badRequest().build();
        }
        
        SensitiveColumn sensitiveColumn = sensitiveDataDetector.detectSensitiveColumn(columnName, dataType);
        return ResponseEntity.ok(sensitiveColumn);
    }
} 
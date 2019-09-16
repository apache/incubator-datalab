package com.epam.dlab.billing.gcp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
@AllArgsConstructor
public class BillingHistory {
	@Id
	private String tableName;
	private final long lastModified;
}

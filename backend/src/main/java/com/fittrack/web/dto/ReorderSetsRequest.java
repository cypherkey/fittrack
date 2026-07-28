package com.fittrack.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReorderSetsRequest(
		@NotEmpty List<@Valid ReorderSetItem> items
) {
}

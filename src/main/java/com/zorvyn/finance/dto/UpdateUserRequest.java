package com.zorvyn.finance.dto;

import com.zorvyn.finance.model.Role;
import lombok.Data;

/**
 * Request body for updating an existing user's role or active status.
 * Both fields are optional; only supplied fields are updated.
 */
@Data
public class UpdateUserRequest {
    private Role role;
    private Boolean active;
}

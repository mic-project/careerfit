// package는 컨트롤러와 같은 계층에 두는 걸 추천
package com.codelab.micproject.auth.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailSendRequest(
        @NotBlank @Email String email
) {}

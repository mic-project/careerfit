package com.codelab.micproject.controller.admin;


import com.codelab.micproject.account.consultant.domain.*;
import com.codelab.micproject.account.consultant.dto.UpsertMetaReq;
import com.codelab.micproject.account.consultant.repository.ConsultantMetaRepository;
import com.codelab.micproject.account.user.domain.*;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin/consultants")
@RequiredArgsConstructor
public class AdminConsultantController {
    private final UserRepository userRepo;
    private final ConsultantMetaRepository metaRepo;


    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/{id}/meta")
    public ApiResponse<Void> upsert(@PathVariable Long id, @RequestBody UpsertMetaReq req){
        var c = userRepo.findById(id).orElseThrow();
        if (c.getRole()!=UserRole.CONSULTANT) throw new IllegalStateException("not a consultant");
        var meta = metaRepo.findByConsultant(c).orElse(new ConsultantMeta());
        meta.setConsultant(c);
        meta.setLevel(ConsultantLevel.valueOf(req.level()));
        meta.setBasePrice(req.basePrice());
        metaRepo.save(meta);
        return ApiResponse.ok();
    }
}
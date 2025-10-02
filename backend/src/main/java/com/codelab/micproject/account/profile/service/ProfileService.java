package com.codelab.micproject.account.profile.service;


import com.codelab.micproject.account.profile.domain.Profile;
import com.codelab.micproject.account.profile.dto.ProfileDto;
import com.codelab.micproject.account.profile.repository.ProfileRepository;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service @RequiredArgsConstructor
public class ProfileService {
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;


    @Transactional(readOnly = true)
    public ProfileDto my(UserPrincipal me){
        var user = userRepository.findById(me.id()).orElseThrow();
        var p = profileRepository.findByUser(user).orElseGet(() -> profileRepository.save(Profile.builder().user(user).build()));
        return new ProfileDto(p.getId(), p.getBio(), p.getSkills(), p.getCareer(), p.getHourlyRate(), p.isPublicCalendar());
    }


    @Transactional
    public ProfileDto upsert(UserPrincipal me, ProfileDto dto){
        var user = userRepository.findById(me.id()).orElseThrow();
        var p = profileRepository.findByUser(user).orElseGet(() -> Profile.builder().user(user).build());
        p.setBio(dto.bio());
        p.setSkills(dto.skills());
        p.setCareer(dto.career());
        p.setHourlyRate(dto.hourlyRate());
        p.setPublicCalendar(dto.publicCalendar());
        profileRepository.save(p);
        return new ProfileDto(p.getId(), p.getBio(), p.getSkills(), p.getCareer(), p.getHourlyRate(), p.isPublicCalendar());
    }
}
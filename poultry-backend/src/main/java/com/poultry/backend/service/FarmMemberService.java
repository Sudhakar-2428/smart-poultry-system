package com.poultry.backend.service;

import com.poultry.backend.dto.FarmMemberResponse;
import com.poultry.backend.dto.JoinRequest;
import com.poultry.backend.entity.FarmRole;

import java.util.List;

public interface FarmMemberService {
    FarmMemberResponse createJoinRequest(JoinRequest request, String currentUserEmail);
    FarmMemberResponse approveMember(Long memberId, String currentUserEmail);
    FarmMemberResponse rejectMember(Long memberId, String currentUserEmail);
    void removeMember(Long memberId, String currentUserEmail);
    FarmMemberResponse changeMemberRole(Long memberId, FarmRole newRole, String currentUserEmail);
    List<FarmMemberResponse> getPendingRequests(String currentUserEmail);
    List<FarmMemberResponse> getFarmMembers(String farmUniqueId, String currentUserEmail);
}

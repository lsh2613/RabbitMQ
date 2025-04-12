package com.rabbitmqprac.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public Member createMember() {
        return memberService.create();
    }

    @GetMapping("/{memberId}")
    public Member getMember(@PathVariable Long memberId) {
        return memberService.getMember(memberId);
    }

    @GetMapping("/members")
    public List<Member> getMembers() {
        return memberService.getMembers();
    }

    @GetMapping("/tokens")
    public String issueAccessToken(@RequestParam Long memberId) {
        return memberService.issueAccessToken(memberId);
    }
}

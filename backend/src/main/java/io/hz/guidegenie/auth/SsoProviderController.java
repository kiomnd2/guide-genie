package io.hz.guidegenie.auth;

import io.hz.guidegenie.auth.dto.SsoProviderDtos.CreateRequest;
import io.hz.guidegenie.auth.dto.SsoProviderDtos.Response;
import io.hz.guidegenie.auth.dto.SsoProviderDtos.UpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * SSO provider 관리 API. 런타임에 provider를 추가/수정/삭제하면 즉시 로그인·API 인증에 반영된다.
 * (운영에서는 관리자 권한으로 제한 권장 — 예: @PreAuthorize)
 */
@RestController
@RequestMapping("/api/admin/sso-providers")
@RequiredArgsConstructor
public class SsoProviderController {

    private final SsoProviderService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response create(@Valid @RequestBody CreateRequest req) {
        return Response.from(service.create(req));
    }

    @GetMapping
    public List<Response> list() {
        return service.findAll().stream().map(Response::from).toList();
    }

    @GetMapping("/{id}")
    public Response get(@PathVariable Long id) {
        return Response.from(service.get(id));
    }

    @PutMapping("/{id}")
    public Response update(@PathVariable Long id, @Valid @RequestBody UpdateRequest req) {
        return Response.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

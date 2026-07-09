package com.gcerp.erp.project;

import com.gcerp.erp.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@CrossOrigin
public class ProjectController {
    private final ProjectService projectService;

    @PostMapping
    public ApiResponse<Project> create(@Valid @RequestBody ProjectCreateRequest req) {
        return ApiResponse.ok(projectService.createProject(req));
    }

    @GetMapping
    public ApiResponse<List<Project>> list(@RequestParam(required = false) Long customerId,
                                           @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(projectService.listProjects(customerId, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<Project> detail(@PathVariable Long id) {
        return ApiResponse.ok(projectService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ApiResponse.ok(true);
    }
}

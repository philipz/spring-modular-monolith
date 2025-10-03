# Requirements: Orders Thymeleaf Removal

## Overview

This specification defines the requirements for removing Thymeleaf template dependencies and resources from the orders module while maintaining API functionality and updating related tests.

## Context & Motivation

The orders module currently contains duplicate Thymeleaf templates and dependencies that conflict with the main monolith application. The module should focus on providing API services rather than UI rendering capabilities.

### Current State Analysis
- **Template files**: 7 HTML template files in `orders/src/main/resources/templates/`
- **Thymeleaf dependencies**: 3 dependencies in `orders/pom.xml`
- **Web controllers**: 2 controllers that return template views (`OrderWebController`, `CartController`)
- **Tests**: Template rendering tests that will need updating

## Functional Requirements

### FR-1: Template File Removal
**As a** system maintainer
**I want** all Thymeleaf template files removed from the orders module
**So that** there are no duplicate templates and the module focuses on API services

**WHEN** removing template files
**THEN** all files in `orders/src/main/resources/templates/` should be deleted
**AND** no HTML template files should remain in the orders module

### FR-2: Dependency Cleanup
**As a** system maintainer
**I want** Thymeleaf dependencies removed from orders/pom.xml
**So that** the module has minimal dependencies and faster build times

**WHEN** cleaning up dependencies
**THEN** the following dependencies should be removed from `orders/pom.xml`:
- `spring-boot-starter-thymeleaf`
- `thymeleaf-layout-dialect`
- `htmx-spring-boot-thymeleaf`

### FR-3: Web Controller Refactoring
**As a** system maintainer
**I want** web controllers converted to REST controllers
**So that** the orders module provides API endpoints instead of template rendering

**WHEN** refactoring controllers
**THEN** `OrderWebController` should be converted to return JSON responses
**AND** `CartController` should be converted to return JSON responses
**AND** template view names should be replaced with appropriate data responses

### FR-4: Test Validation
**As a** system maintainer
**I want** all tests to pass after template removal
**So that** the refactoring doesn't break existing functionality

**WHEN** running tests after changes
**THEN** all existing API tests should continue to pass
**AND** any template-specific tests should be updated or removed
**AND** no test failures should occur related to missing templates

## Non-Functional Requirements

### NFR-1: Backwards Compatibility
- API endpoints must maintain the same URLs and response structures
- REST API functionality must remain unchanged
- No breaking changes to public API contracts

### NFR-2: Build Performance
- Build time should improve due to fewer dependencies
- Module should start faster without template processing overhead

### NFR-3: Code Quality
- All code should follow existing project conventions
- No unused imports or dependencies should remain
- Code formatting should be maintained using Spotless

## Technical Constraints

### TC-1: Module Architecture
- Orders module should remain a separate Maven module
- Spring Modulith structure must be preserved
- API interfaces must remain unchanged

### TC-2: Main Application Impact
- Changes should not affect the main monolith application
- Template functionality should remain available in the main application
- No impact on other modules (catalog, inventory, notifications)

### TC-3: Test Framework
- Existing test framework structure should be maintained
- Integration tests should continue to work
- Test containerization should remain functional

## Acceptance Criteria

### AC-1: File Structure Validation
- [ ] No template files exist in `orders/src/main/resources/templates/`
- [ ] No Thymeleaf dependencies in `orders/pom.xml`
- [ ] All web controllers return JSON responses

### AC-2: Functionality Preservation
- [ ] All REST API endpoints continue to work
- [ ] Order creation, retrieval, and listing functionality is preserved
- [ ] Cart functionality is preserved through API calls

### AC-3: Test Coverage
- [ ] All existing integration tests pass
- [ ] No template rendering tests remain
- [ ] API tests validate JSON response structures

### AC-4: Build Quality
- [ ] Maven build completes successfully
- [ ] Spotless formatting checks pass
- [ ] No compilation errors or warnings

## Success Metrics

1. **Build Performance**: Build time reduction of 10-20% due to fewer dependencies
2. **Code Coverage**: Maintain existing test coverage levels
3. **API Compatibility**: 100% of existing API endpoints remain functional
4. **Test Success Rate**: 100% of tests pass after refactoring

## Dependencies & Integration Points

### Internal Dependencies
- **Catalog Module**: Orders module calls catalog APIs for product validation
- **Main Application**: Template functionality moves to main app only
- **API Contracts**: OrdersApi, OrderDto, OrderView interfaces must remain stable

### External Dependencies
- PostgreSQL database integration must be preserved
- RabbitMQ messaging must continue to work
- Hazelcast caching functionality must be maintained

## Risk Assessment

### High Risk
- **Template removal breaking integration tests**: Mitigation - thorough testing
- **Controller refactoring changing API behavior**: Mitigation - maintain exact same endpoints

### Medium Risk
- **Dependency conflicts with main application**: Mitigation - careful dependency management
- **Test failures due to missing Thymeleaf**: Mitigation - update test frameworks

### Low Risk
- **Build performance issues**: Mitigation - should improve with fewer dependencies
- **Code formatting violations**: Mitigation - run Spotless before commit

## Out of Scope

- Modifying the main monolith application templates
- Changing API contract structures or endpoints
- Refactoring other modules (catalog, inventory, notifications)
- Adding new functionality beyond template removal
- Performance optimization beyond dependency cleanup
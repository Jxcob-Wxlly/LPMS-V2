using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lpms.Api.Controllers;

/// <summary>Base class: every endpoint requires a valid JWT unless it is marked [AllowAnonymous].</summary>
[ApiController]
[Authorize]
[Produces("application/json")]
public abstract class ApiControllerBase : ControllerBase
{
    protected ObjectResult BadRequestProblem(string detail) =>
        Problem(detail: detail, statusCode: StatusCodes.Status400BadRequest, title: "Invalid request");

    protected ObjectResult NotFoundProblem(string detail) =>
        Problem(detail: detail, statusCode: StatusCodes.Status404NotFound, title: "Not found");
}

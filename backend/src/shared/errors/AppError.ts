export class AppError extends Error {
  public readonly statusCode: number;
  public readonly isOperational: boolean;

  constructor(message: string, statusCode = 500) {
    super(message);

    this.name = this.constructor.name;
    this.statusCode = statusCode;
    this.isOperational = true;

    Object.setPrototypeOf(this, new.target.prototype);

    Error.captureStackTrace(this, this.constructor);
  }

  static badRequest(message = "Bad Request") {
    return new AppError(message, 400);
  }

  static unauthorized(message = "Unauthorized") {
    return new AppError(message, 401);
  }

  static forbidden(message = "Forbidden") {
    return new AppError(message, 403);
  }

  static notFound(message = "Resource not found") {
    return new AppError(message, 404);
  }

  static conflict(message = "Conflict") {
    return new AppError(message, 409);
  }

  static unprocessable(message = "Unprocessable Entity") {
    return new AppError(message, 422);
  }

  static internal(message = "Internal Server Error") {
    return new AppError(message, 500);
  }
}

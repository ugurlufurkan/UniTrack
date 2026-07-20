import { Request, Response, NextFunction } from "express";
import { ZodError, ZodType } from "zod";

export const validate =
  (schema: ZodType) =>
  (req: Request, res: Response, next: NextFunction) => {
    try {
      schema.parse(req.body);
      next();
    } catch (error) {
      if (error instanceof ZodError) {
        return res.status(400).json({
          success: false,
          message: "Validation Error",
          errors: error.issues,
        });
      }
      next(error);
    }
  };
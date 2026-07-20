import { Request, Response } from "express";
import * as transcriptService from "./transcript.service";

export const getTranscript = async (
  req: Request,
  res: Response
) => {
  const transcript = await transcriptService.getTranscript(
    req.user!.userId
  );

  res.json(transcript);
};
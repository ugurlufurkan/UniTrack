import { Request, Response } from "express";

import * as contentService from "./content.service";

export const getAnnouncements = async (_req: Request, res: Response) => {
  res.json(await contentService.getAnnouncements());
};

export const getFaqs = async (_req: Request, res: Response) => {
  res.json(await contentService.getFaqs());
};

export const getTips = async (_req: Request, res: Response) => {
  res.json(await contentService.getTips());
};

export const getPage = async (req: Request, res: Response) => {
  const slug = String(req.params.slug);
  res.json(await contentService.getPage(slug));
};

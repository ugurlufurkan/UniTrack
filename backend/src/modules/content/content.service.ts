import * as repository from "./content.repository";

export const getAnnouncements = () => repository.findActiveAnnouncements();

export const getFaqs = () => repository.findActiveFaqs();

export const getTips = () => repository.findActiveTips();

export const getPage = (slug: string) => repository.findPageBySlug(slug);

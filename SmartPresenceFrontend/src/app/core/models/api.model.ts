/**
 * Enveloppe de réponse uniforme renvoyée par toutes les API du backend.
 * Voir SmartPresence_CONTEXT.md §11.1.
 */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  /** Absent lorsque l'opération ne renvoie aucune charge utile. */
  data?: T;
  timestamp: string;
}

/** Enveloppe des résultats paginés. */
export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

/** Erreur métier normalisée, telle que présentée à l'utilisateur. */
export interface ApiError {
  message: string;
  status: number;
}

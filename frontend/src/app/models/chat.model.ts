export interface ChatRequest {
  message: string;
  conversationId?: string;
  userId?: string;
  userName?: string;
  country?: string;
}

export interface Citation {
  documentId?: string;
  documentName: string;
  sourcePath?: string;
  section?: string;
  pageNumber?: number;
  webUrl?: string;
}

export interface ChatResponse {
  answer: string;
  citations: Citation[];
  confidenceScore: number;
  detectedLanguage?: string;
  answered: boolean;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  citations?: Citation[];
  confidenceScore?: number;
  timestamp: Date;
  loading?: boolean;
}

export interface ConversationDto {
  id: string;
  teamsConversationId: string;
  userId: string;
  userName?: string;
  userCountry?: string;
  question: string;
  response?: string;
  confidenceScore?: number;
  timestamp: string;
}

export interface AnalyticsSummary {
  totalQuestions: number;
  answeredQuestions: number;
  unansweredQuestions: number;
  answerRate: number;
  avgResponseTimeMs?: number;
  avgConfidenceScore?: number;
  recentEntries: AnalyticsEntry[];
}

export interface AnalyticsEntry {
  id: string;
  conversationId?: string;
  responseTimeMs?: number;
  confidenceScore?: number;
  questionLanguage?: string;
  userCountry?: string;
  wasAnswered: boolean;
  timestamp: string;
}

export const AI_CHAT_SESSION_KEY = 'ai_chat_session';

export function shouldRestoreAiChatState(snapshot) {
  if (!snapshot || typeof snapshot !== 'object') {
    return false;
  }

  const hasMessages = Array.isArray(snapshot.messages) && snapshot.messages.length > 0;
  const hasAnswerText = Boolean(String(snapshot.answerText || '').trim());
  const hasCards = Array.isArray(snapshot.cards) && snapshot.cards.length > 0;
  const hasQuestion = Boolean(String(snapshot.lastQuestion || snapshot.question || '').trim());

  return hasMessages || hasAnswerText || hasCards || hasQuestion;
}

export function buildPersistedAiChatState(data = {}) {
  const isStreaming = data.status === 'streaming';
  const messages = Array.isArray(data.messages)
    ? data.messages.map((message) => ({
        id: message.id,
        role: message.role,
        rawText: String(message.rawText || ''),
        cards: Array.isArray(message.cards) ? message.cards.map((card) => ({
          ...card,
          animateIn: false
        })) : [],
        isStreaming: false
      }))
    : [];

  return {
    question: String(data.question || ''),
    messages,
    answerText: String(data.answerText || ''),
    status: isStreaming ? 'interrupted' : (data.status || 'idle'),
    statusText: isStreaming ? '已恢复上次对话记录' : (data.statusText || '已恢复上次对话记录'),
    cards: Array.isArray(data.cards) ? data.cards : [],
    loading: false,
    requestId: data.requestId || '',
    messageId: data.messageId || '',
    lastQuestion: data.lastQuestion || '',
    lastRejectedPoiIds: Array.isArray(data.lastRejectedPoiIds) ? data.lastRejectedPoiIds : [],
    preferenceSummary: data.preferenceSummary || '',
    profileLoading: false,
    activeAssistantMessageId: ''
  };
}

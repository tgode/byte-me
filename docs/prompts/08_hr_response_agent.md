Implement the HR Response Agent.

Requirements:

Input:

* question
* country
* retrieved chunks

Rules:

* Use only retrieved context
* Never invent company policies
* Reject unrelated questions
* Include citations
* Apply country filtering

Output:

answer
citations
confidence

Low confidence message:

"I could not find a reliable answer. Please contact HR."

Generate implementation.

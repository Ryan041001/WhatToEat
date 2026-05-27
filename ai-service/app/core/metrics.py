import threading
from typing import Union


class RequestMetrics:
    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._request_count = 0
        self._error_count = 0
        self._total_response_time_ms = 0.0

    def record(self, status_code: int, response_time_ms: float) -> None:
        with self._lock:
            self._request_count += 1
            self._total_response_time_ms += response_time_ms
            if status_code >= 500:
                self._error_count += 1

    def snapshot(self) -> dict[str, Union[float, int]]:
        with self._lock:
            request_count = self._request_count
            error_count = self._error_count
            average_response_time_ms = (
                self._total_response_time_ms / request_count if request_count else 0.0
            )
            error_rate = error_count / request_count if request_count else 0.0

        return {
            "requestCount": request_count,
            "errorCount": error_count,
            "averageResponseTimeMs": round(average_response_time_ms, 3),
            "errorRate": round(error_rate, 4),
        }


metrics = RequestMetrics()

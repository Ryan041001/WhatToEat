class ModelServiceError(RuntimeError):
    pass


class ModelTimeoutError(ModelServiceError):
    pass


class ModelResponseError(ModelServiceError):
    pass

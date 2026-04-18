import unittest

from app.config import normalize_openai_base_url


class ConfigTest(unittest.TestCase):
    def test_should_append_v1_when_missing(self) -> None:
        self.assertEqual(
            "https://www.right.codes/codex/v1",
            normalize_openai_base_url("https://www.right.codes/codex"),
        )

    def test_should_keep_existing_v1(self) -> None:
        self.assertEqual(
            "https://api.openai.com/v1",
            normalize_openai_base_url("https://api.openai.com/v1"),
        )


if __name__ == "__main__":
    unittest.main()

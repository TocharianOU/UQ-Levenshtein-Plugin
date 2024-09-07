# UQ-Levenshtein Similarity Search

This project implements the **UQ-Levenshtein Algorithm**, an enhanced version of the Damerau-Levenshtein algorithm. UQ-Levenshtein integrates **Jaccard similarity** and a custom **letter and number similarity vector** to improve the accuracy of fuzzy searches. This algorithm is particularly useful in scenarios like vehicle license plates and store name searches, where small spelling differences or input errors are common.

## Algorithm Overview

The **UQ-Levenshtein Algorithm** combines three core components:

1. **Damerau-Levenshtein Distance**:
   - A classical algorithm for calculating the minimum number of operations (insertions, deletions, substitutions, and transpositions) needed to transform one string into another.
   - It is used to assess how much two strings differ by counting the operations needed to convert one string to another.

2. **Jaccard Similarity**:
   - Measures the similarity between two sets of characters by calculating the size of their intersection divided by the size of their union.
   - This set-based similarity metric is useful for comparing the overall character composition between two strings.

3. **Letter and Number Similarity Vector**:
   - A custom similarity score is assigned based on the visual or phonetic resemblance between letters and numbers (e.g., `O` and `0`, `1` and `I`).
   - This feature makes the UQ-Levenshtein algorithm highly effective in handling common data entry errors, such as when similar-looking characters are confused (especially in license plates or store names).

## Query Example

The following is an example query using the UQ-Levenshtein algorithm for Elasticsearch:

```json
{
  "query": {
    "script_score": {
      "query": {
        "bool": {
          "filter": [
            {
              "term": {
                "plateType": "prefix_2_num"
              }
            }
          ],
          "must": [
            {
              "match": {
                "plateNumber": {
                  "query": "AZMAT"
                }
              }
            }
          ]
        }
      },
      "script": {
        "lang": "expert_scripts",
        "source": "uq_levenshtein_score",
        "params": {
          "field": "originalNumber",
          "term": "AZMAT",
          "alpha": 0.7,
          "beta": 0.3,
          "max_dist": 4
        }
      }
    }
  }
}
```

### Explanation of Query

The query searches for records where the `plateType` is `"prefix_2_num"`. It performs a match on the `plateNumber` field with the query term `"AZMAT"`, and then uses the **UQ-Levenshtein Algorithm** to compute a similarity score between the `originalNumber` field in the documents and the search term.

- **filter**: The filter ensures that only documents with a specific `plateType` (in this case, `"prefix_2_num"`) are considered for scoring.
- **must**: The `must` clause performs a text match query on the `plateNumber` field, matching documents that contain or closely resemble the query term `"AZMAT"`.
- **script_score**: This custom scoring function uses the **UQ-Levenshtein Algorithm** to calculate a similarity score for each document. It compares the `originalNumber` field from the document with the query term, `"AZMAT"`, and computes a score based on the Damerau-Levenshtein distance, Jaccard similarity, and a custom similarity vector for letters and numbers.

### Parameters:
- **field**: The document field to compare against the query term, in this case, `originalNumber`.
- **term**: The search term being used, `"AZMAT"`.
- **alpha**: The weight given to the Damerau-Levenshtein distance in the final score.
- **beta**: The weight given to the Jaccard similarity in the final score.
- **max_dist**: The maximum allowed edit distance (threshold for Damerau-Levenshtein).

The result is a custom similarity score, which allows fuzzy matching of terms that may have minor spelling errors or transpositions, making it ideal for searches in contexts like vehicle license plates or store names.
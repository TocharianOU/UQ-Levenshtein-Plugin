# UQ-Levenshtein Algorithm

The **UQ-Levenshtein Algorithm** is an advanced text similarity algorithm that enhances the Damerau-Levenshtein distance metric with Jaccard similarity and a custom character similarity vector. Designed for high-accuracy fuzzy search, it’s especially useful in scenarios with common spelling errors or input mistakes. The algorithm is compatible with **Elasticsearch 8.7.0** and above.

## Use Cases
- **Vehicle License Plate Bidding Websites**: Improves accuracy in searching for registration numbers, even with minor variations or errors.
- **Store Name Searches in E-Commerce Platforms**: Enhances search accuracy by compensating for typographical errors, helping customers find stores even with slight mistakes.

This plugin is ideal for applications needing high accuracy in data retrieval and error compensation, suitable for both commercial and law enforcement purposes.

---

## Algorithm Overview

The UQ-Levenshtein Algorithm incorporates three core components to improve text matching accuracy:

1. **Damerau-Levenshtein Distance**: Calculates the minimum number of operations (insertions, deletions, substitutions, and transpositions) required to transform one string into another. Uses a **custom similarity matrix** to adjust substitution penalties dynamically.
  
2. **Custom Letter and Number Similarity Vector**: Applies reduced penalties for substituting visually similar characters, improving accuracy where similar-looking characters are commonly mistaken.

3. **Jaccard Similarity**: Calculates similarity based on the ratio of intersection to union of character sets in two strings, complementing the Damerau-Levenshtein distance with set-based similarity.

### Custom Similarity Matrix

The matrix adjusts substitution penalties based on visual similarity, especially effective in applications involving mixed letters and numbers. Below is a sample matrix:

| Character 1 | Character 2 | Similarity Score |
|-------------|-------------|------------------|
| A           | 4           | 0.8             |
| B           | 8           | 0.9             |
| B           | 3           | 0.6             |
| D           | O           | 0.8             |
| D           | 0           | 0.8             |
| E           | 3           | 0.7             |
| G           | 6           | 0.8             |
| C           | G           | 0.5             |
| I           | 1           | 0.95            |
| I           | L           | 0.6             |
| O           | 0           | 0.9             |
| O           | Q           | 0.7             |
| S           | 5           | 0.9             |
| Z           | 2           | 0.85            |
| T           | 7           | 0.85            |
| L           | 1           | 0.85            |
| P           | R           | 0.6             |
| U           | V           | 0.75            |
| V           | Y           | 0.5             |
| M           | N           | 0.45            |
| K           | X           | 0.5             |

---

## Parameters

- **alpha**: Controls the weight of Damerau-Levenshtein distance in the final score. A higher alpha emphasizes character replacement similarity.
- **beta**: Controls the weight of Jaccard similarity in the final score. A higher beta emphasizes set-based similarity.
- **max_dist**: Limits the maximum allowable distance for fuzzy matching.
- **window_size**: Defines the scope of the search window, optimizing performance for larger datasets or longer strings.

---

## Installation

To integrate the UQ-Levenshtein Algorithm into your Elasticsearch environment, follow these steps:

1. [Download the latest release](https://github.com/TocharianOU/UQ-Levenshtein-Plugin/releases/download/all/uq_levenshtein-similarity-plugin-1.0.0.zip) from the GitHub Releases page.
2. Extract the ZIP file to your Elasticsearch plugins directory.
3. Restart Elasticsearch to load the new plugin.
4. Verify the plugin is loaded correctly by running the Elasticsearch plugin list command: bin/elasticsearch-plugin list
5. For detailed installation instructions, please visit our [installation guide](https://github.com/TocharianOU/UQ-Levenshtein-Plugin/releases/tag/all).


## Example Query 1

The following is an example query using the UQ-Levenshtein algorithm for Elasticsearch:

```json
GET carplate/_search
{
  "query": {
    "script_score": {
      "query": {
        "match_all": {}
      },
      "script": {
        "lang": "similarity_custom",
        "source": "uq_score",
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

## Search Results 1

After running the above query, Elasticsearch returns the following results, demonstrating the effectiveness of the UQ-Levenshtein Algorithm in handling fuzzy search scenarios:
alpha is 0.7,
beta is 0.3

```json
{
  "took": 149,
  "hits": {
    "hits": [
      {"_score": 0.6075814, "_source": {"plateNumber": "A27 MAT"}},
      {"_score": 0.6045814, "_source": {"plateNumber": "A24 MAT"}},
      {"_score": 0.5642222, "_source": {"plateNumber": "Z27 MAT"}},
      {"_score": 0.5612222, "_source": {"plateNumber": "Z24 MAT"}},
      {"_score": 0.5565814, "_source": {"plateNumber": "A22 MAT"}},
      {"_score": 0.5402222, "_source": {"plateNumber": "N27 MAT"}},
      {"_score": 0.5372222, "_source": {"plateNumber": "N24 MAT"}},
      {"_score": 0.5325098, "_source": {"plateNumber": "A27 NAT"}},
      {"_score": 0.5295098, "_source": {"plateNumber": "A24 NAT"}},
      {"_score": 0.51808137, "_source": {"plateNumber": "A23 MAT"}}
    ]
  }
}
```

## Example Query 2

The following is an example query using the UQ-Levenshtein algorithm for Elasticsearch:
alpha is 0.75,
beta is 0.25

```json
GET suffix/_search?filter_path=hits.hits._source.plateNumber
{
  "size": 30,
  "query": {
    "script_score": {
      "query": {
        "match_all": {}
      },
      "script": {
        "lang": "similarity_custom",
        "source": "uq_score",
        "params": {
          "field": "plateNumber.keyword",
          "term": "AZMAT",
          "alpha": 0.8,
          "beta": 0.2,
          "max_dist": 6,
          "window_size": 500000,
          "from": 0
        }
      }
    }
  }
}
```

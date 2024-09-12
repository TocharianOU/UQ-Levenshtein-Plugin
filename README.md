# UQ-Levenshtein Algorithm

This project implements the **UQ-Levenshtein Algorithm**, an enhanced version of the Damerau-Levenshtein algorithm. By integrating **Jaccard similarity** and a custom **letter and number similarity vector**, the UQ-Levenshtein Algorithm improves the accuracy of fuzzy searches. This algorithm is particularly useful in scenarios where small spelling differences or input errors are common. The UQ-Levenshtein plugin is designed to work with **Elasticsearch 8.7.0 and above**, ensuring compatibility with the latest versions and features of Elasticsearch.


## Use Cases

- **Vehicle License Plate Bidding Websites:** Improves the accuracy of searches for vehicle registration numbers on license plate bidding websites, helping users find plate numbers that visually or phonetically match their desired specifications, even with slight input mistakes or variations.
- **Store Name Searches in E-Commerce Platforms:** Enhances the ability of e-commerce platforms to find store names within their databases, especially useful where typographical errors are common, helping customers find stores that match their intended search, even if the store names are not spelled exactly.

This plugin provides an essential tool for systems requiring high levels of accuracy in data retrieval and entry error compensation, making it ideal for various applications in commercial and law enforcement settings.


## Algorithm Overview

The **UQ-Levenshtein Algorithm** integrates three core components, each contributing uniquely to the process of text matching:

1. **Damerau-Levenshtein Distance**:
   - This classical algorithm calculates the minimum number of operations—insertions, deletions, substitutions, and transpositions—required to transform one string into another.
   - The algorithm adjusts the substitution penalty dynamically using a custom similarity matrix. This adjustment specifically affects the calculation of the Damerau-Levenshtein distance and does not influence other algorithm components.

2. **Custom Letter and Number Similarity Vector**:
   - The algorithm assigns reduced penalties for substituting characters based on their visual resemblance, enhancing accuracy in scenarios where similar-looking characters are often mistaken. 


3. **Jaccard Similarity**:
   - This metric evaluates the similarity between two sets of characters by calculating the ratio of the intersection to the union of the sets.
   - The calculation of Jaccard similarity is independent of the custom similarity matrix.

### Custom Similarity Matrix

The custom similarity matrix dynamically adjusts the substitution penalties in the Damerau-Levenshtein distance calculation, ensuring that the penalties reflect the true nature of the character differences. Below is a detailed table illustrating the similarity scores between various character pairs:

| Character 1 | Character 2 | Similarity Score |
|-------------|-------------|------------------|
| A           | 4           | 0.8              |
| B           | 8           | 0.9              |
| B           | 3           | 0.6              |
| D           | O           | 0.8              |
| D           | 0           | 0.8              |
| E           | 3           | 0.7              |
| G           | 6           | 0.8              |
| C           | G           | 0.5              |
| I           | 1           | 0.95             |
| I           | L           | 0.6              |
| O           | 0           | 0.9              |
| O           | Q           | 0.7              |
| S           | 5           | 0.9              |
| Z           | 2           | 0.85             |
| T           | 7           | 0.85             |
| L           | 1           | 0.85             |
| P           | R           | 0.6              |
| U           | V           | 0.75             |
| V           | Y           | 0.5              |
| M           | N           | 0.45             |
| K           | X           | 0.5              |




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
              },
      "query": {
        "bool": {
          "must": [
            {
              "match": {
                "plateNumber.phonetic": "AZMAT"
                
              }
            }
          ], 
          "filter": [
            {
              "term": {
                "plateType": "suffix_1_num"
              }
            }
          ]
        }
      }
    }
  }
}
```

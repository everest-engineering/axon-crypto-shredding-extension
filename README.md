# Crypro shredding extensions for Axon
[![Build Status](https://travis-ci.com/everest-engineering/axon-crypto-shredding-extension.svg?branch=master)](https://travis-ci.com/everest-engineering/axon-crypto-shredding-extension) ![Latest release](https://img.shields.io/github/v/release/everest-engineering/axon-crypto-shredding-extension)

This is a supporting repository for [Lhotse](https://github.com/everest-engineering/lhotse), a starter kit for writing 
event sourced web applications following domain driven design principles.

This library implements crypto shredding for the Axon event log and saga store. Crypto shredding is a technique for 
disabling access to sensitive information by discarding encryption keys. You might use this on the behest of a user or 
when retention is no longer justified in order to comply with the European Union's General Data Protection Regulation (GDPR)
without compromising the append-only nature of your event log.  

## How it works
The extension wraps the existing Axon serializers and intercepts fields annotated with `@EncryptedField`, encrypting them
with symmetric keys that are generated and stored alongside the Axon event log and saga store. Encryption keys
are identified via the `@EncryptionKeyIdentifier` annotation. This annotation accepts an optional `keyType` parameter
that is used to differentiate between identifiers when key uniqueness cannot be globally guaranteed (such as when using 
monotonically increasing integers).          

A 256 bit AES (symmetric) key is generated for each {identifier, `keyType`} tuple. Each field annotated with `@EncryptedField` is 
encrypted using an initialisation vector unique to that field. This initialisation vector is stored as part of the serialised
field payload.    

## Shredding data
The `CryptoShreddingKeyService`'s `deleteSecretKey` method should be called to discard a secret key. The encryption key 
table should not be modified directly.

Once a key has been discarded, deserialisation will return fields with default values for the field's data type.

Projections can be cleaned up through event handlers that perform an appropriate action -- delete or scrub -- given your domain.

Aggregate snapshots may need to be dropped and saga lifecycles may need to be completed.

## Limitations
Currently, there is a 1-to-1 constraint on the serialisation payload and the encryption key. That is, only a single
`@EncryptionKeyIdentifier` is supported per event or saga. 

## Caveat
This library (or a similar implementation) is *required* to deserialise annotated events once encryption has been applied.
It is not sufficient to remove this library as a dependency as the underlying data types in the serialised form do not 
align with the types declared for an event or saga.


## License
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

[![License: EverestEngineering](https://img.shields.io/badge/Copyright%20%C2%A9-EVERESTENGINEERING-blue)](https://everest.engineering)

>Talk to us `hi@everest.engineering`.

package com.packagetracking.command.client;

import com.packagetracking.command.constants.ExternalApiConstants;
import com.packagetracking.command.dto.external.DogFact;
import com.packagetracking.command.dto.external.DogFactResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class DogApiFallback implements DogApiClient {
    
    @Override
    public DogFactResponse getDogFacts(Integer limit) {

        log.warn(ExternalApiConstants.DOG_API_FALLBACK_MESSAGE);
        
        List<DogFact> fallbackFacts = List.of(
            new DogFact(
                ExternalApiConstants.DOG_FALLBACK_ID_PREFIX + "1",
                ExternalApiConstants.DOG_FACT_TYPE,
                new DogFact.DogFactAttributes("Cachorros são incríveis companheiros!")
            ),
            new DogFact(
                ExternalApiConstants.DOG_FALLBACK_ID_PREFIX + "2", 
                ExternalApiConstants.DOG_FACT_TYPE,
                new DogFact.DogFactAttributes("Os cães têm um olfato 40 vezes melhor que os humanos.")
            ),
            new DogFact(
                ExternalApiConstants.DOG_FALLBACK_ID_PREFIX + "3",
                ExternalApiConstants.DOG_FACT_TYPE, 
                new DogFact.DogFactAttributes("Cachorros podem aprender mais de 100 palavras.")
            )
        );
        
        if (limit != null && limit > 0 && limit < fallbackFacts.size()) {
            fallbackFacts = fallbackFacts.subList(0, limit);
        }
        
        return new DogFactResponse(fallbackFacts);
    }
} 
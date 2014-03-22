        // Pass-2: AA on Texture
        // Note: gcv_TexCoord is in center of sample pixels.

        const float sampleCount = gcu_TextureSize.z;
        const vec2 psize = 1.0 / gcu_TextureSize.xy; // pixel size

        // Just poles (NW, SW, ..)
        const float pixelCount = 2 * sampleCount;

        // sampleCount [0, 1, 3, 5, 7] are undefined!
        const float layerCount = ( sampleCount / 2.0 );

        // sum of all integer [layerCount .. 1] -> Gauss
        const float denom = ( layerCount / 2.0 ) * ( layerCount + 1.0 );

        const vec2 texCoord = gcv_TexCoord.st;

        vec4 t = vec4(0);

        // Layer-1: SampleCount 2 -> 4x
        t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-0.5, -0.5))); // NW
        t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-0.5,  0.5))); // SW
        t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 0.5,  0.5))); // SE
        t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 0.5, -0.5))); // NE

        t *= (layerCount - 0.0) / ( denom * 4.0 ); // weight layer 1

        if( sampleCount > 2 ) {
            // Layer-2: SampleCount 4 -> +4x = 8p
            vec4 tn = vec4(0);
            tn  = texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-1.5, -1.5))); // NW
            tn += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-1.5,  1.5))); // SW
            tn += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 1.5,  1.5))); // SE
            tn += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 1.5, -1.5))); // NE

            t += tn * (layerCount - 1) / ( denom * 4.0 ); // weight layer 2

            if( sampleCount > 4 ) {
                // Layer-3: SampleCount 6 -> +4 = 12p
                tn  = texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-2.5, -2.5))); // NW
                tn += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-2.5,  2.5))); // SW
                tn += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 2.5,  2.5))); // SE
                tn += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 2.5, -2.5))); // NE

                t += tn * (layerCount - 2) / ( denom * 4.0 ); // weight layer 3

                if( sampleCount > 6 ) {
                    // Layer-4: SampleCount 8 -> +4 = 16p
                    tn  = texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-3.5, -3.5))); // NW
                    tn += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-3.5,  3.5))); // SW
                    tn += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 3.5,  3.5))); // SE
                    tn += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 3.5, -3.5))); // NE

                    t += tn * (layerCount - 3) / ( denom * 4.0 ); // weight layer 4
                }
            }
        }

        #if 0
        if(t.w == 0.0){
            discard; // discard freezes NV tegra2 compiler
        }
        #endif
        
        color = t.rgb;
        alpha = gcu_Alpha * t.a;
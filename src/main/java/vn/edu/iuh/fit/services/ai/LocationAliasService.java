package vn.edu.iuh.fit.services.ai;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entities.District;
import vn.edu.iuh.fit.entities.Province;
import vn.edu.iuh.fit.repositories.DistrictRepository;
import vn.edu.iuh.fit.repositories.ProvinceRepository;
import vn.edu.iuh.fit.utils.text.VietnameseNormalizer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class LocationAliasService {

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;

    private final Map<String, Province> provinceAliases = new ConcurrentHashMap<>();
    private final Map<String, District> districtAliases = new ConcurrentHashMap<>();

    @PostConstruct
    @Transactional(readOnly = true)
    public void preload() {
        List<Province> provinces = provinceRepository.findAll();
        for (Province province : provinces) {
            registerProvinceAlias(province, province.getName_with_type());
            String normalized = VietnameseNormalizer.normalize(province.getName_with_type());
            if (normalized != null) {
                registerProvinceAlias(province, normalized);
                registerProvinceAlias(province, normalized.replace(" ", ""));
            }
            for (String alias : aliasForProvince(province)) {
                registerProvinceAlias(province, alias);
            }
        }

        List<District> districts = districtRepository.findAll();
        for (District district : districts) {
            registerDistrictAlias(district, district.getName_with_type());
            String normalized = VietnameseNormalizer.normalize(district.getName_with_type());
            if (normalized != null) {
                registerDistrictAlias(district, normalized);
                registerDistrictAlias(district, normalized.replace(" ", ""));
                registerDistrictAlias(district, compactDistrictAlias(normalized));
            }
        }
    }

    private void registerProvinceAlias(Province province, String alias) {
        if (alias == null || alias.isBlank()) return;
        String key = VietnameseNormalizer.normalize(alias);
        if (key == null || key.isBlank()) return;
        provinceAliases.putIfAbsent(key, province);
    }

    private void registerDistrictAlias(District district, String alias) {
        if (alias == null || alias.isBlank()) return;
        String key = VietnameseNormalizer.normalize(alias);
        if (key == null || key.isBlank()) return;
        districtAliases.putIfAbsent(key, district);
    }

    private Collection<String> aliasForProvince(Province province) {
        String name = province.getName_with_type();
        if (name == null) return List.of();
        String normalized = VietnameseNormalizer.normalize(name);
        if (normalized == null) return List.of();
        List<String> aliases = new ArrayList<>();
        if (normalized.contains("ho chi minh")) {
            aliases.add("hcm");
            aliases.add("tphcm");
            aliases.add("sg");
            aliases.add("sai gon");
        }
        if (normalized.contains("ha noi")) {
            aliases.add("hn");
            aliases.add("tphn");
        }
        if (normalized.contains("da nang")) {
            aliases.add("dn");
        }
        if (normalized.contains("hai phong")) {
            aliases.add("hp");
        }
        return aliases;
    }

    private String compactDistrictAlias(String normalizedName) {
        if (normalizedName == null) return null;
        if (normalizedName.startsWith("quan ")) {
            String digits = normalizedName.substring(5).replaceAll("[^0-9]", "");
            if (!digits.isBlank()) {
                return "q" + digits;
            }
        }
        if (normalizedName.startsWith("huyen ")) {
            String digits = normalizedName.substring(6).replaceAll("[^0-9]", "");
            if (!digits.isBlank()) {
                return "h" + digits;
            }
        }
        return null;
    }

    public Optional<LocationMatch> resolve(String provinceHint, String districtHint) {
        Province province = lookupProvince(provinceHint);
        District district = lookupDistrict(districtHint);

        if (district != null && (province == null || !Objects.equals(district.getProvince().getCode(), province.getCode()))) {
            province = district.getProvince();
        }

        if (province != null && district != null
                && !Objects.equals(district.getProvince().getCode(), province.getCode())) {
            district = null; // incompatible province/district combination
        }

        if (province == null && district == null) {
            return Optional.empty();
        }
        return Optional.of(new LocationMatch(
                province != null ? province.getCode() : null,
                province != null ? province.getName_with_type() : null,
                district != null ? district.getCode() : null,
                district != null ? district.getName_with_type() : null
        ));
    }

    private Province lookupProvince(String hint) {
        if (hint == null || hint.isBlank()) return null;
        String normalized = VietnameseNormalizer.normalize(hint);
        if (normalized == null || normalized.isBlank()) return null;
        Province direct = provinceAliases.get(normalized);
        if (direct != null) return direct;
        return provinceAliases.entrySet().stream()
                .filter(entry -> entry.getKey().contains(normalized) || normalized.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private District lookupDistrict(String hint) {
        if (hint == null || hint.isBlank()) return null;
        String normalized = VietnameseNormalizer.normalize(hint);
        if (normalized == null || normalized.isBlank()) return null;
        District direct = districtAliases.get(normalized);
        if (direct != null) return direct;
        return districtAliases.entrySet().stream()
                .filter(entry -> entry.getKey().contains(normalized) || normalized.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public record LocationMatch(String provinceCode,
                                String provinceName,
                                String districtCode,
                                String districtName) {}
}

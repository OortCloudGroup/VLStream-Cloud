package com.ruoyi.vlstream.test.vlstream.compute;

import com.jcraft.jsch.HostKey;
import com.ruoyi.vlstream.test.vlstream.data.PublicDatasetDownloader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Only the two approved company SSH assets may bypass the public-address restriction. */
@Component
@RequiredArgsConstructor
public class ComputeTrustedSelfPolicy {
    private static final Set<String> APPROVED_HOSTS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList("192.168.88.173", "192.168.88.99")));
    private static final Set<String> MODERN_HOST_KEYS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList("ssh-ed25519", "ecdsa-sha2-nistp256", "ecdsa-sha2-nistp384", "ecdsa-sha2-nistp521")));
    private final ComputeTrustedSelfAssetsProperties properties;
    public boolean isApprovedHost(String host) { return APPROVED_HOSTS.contains(host); }

    /** Legacy SSH call sites have no node row; still require the exact configured asset and pin. */
    public Target resolveLegacy(String host, int port, String username) throws IOException {
        if (!isApprovedHost(host)) throw new IOException("不是已批准的自有主机");
        List<ComputeTrustedSelfAssetsProperties.Asset> assets = properties.getAssets();
        if (assets == null) throw new IOException("受信自有资产未配置");
        ComputeTrustedSelfAssetsProperties.Asset matched = null;
        for (ComputeTrustedSelfAssetsProperties.Asset asset : assets) {
            if (asset != null && host.equals(asset.getHost())) matched = asset;
        }
        if (matched == null || port != 22 || !username.equals(matched.getUsername()))
            throw new IOException("旧SSH调用未匹配已批准资产端点");
        ComputeNode node = new ComputeNode();
        node.setProvider("self"); node.setHost(host); node.setPort(port);
        node.setUsername(username); node.setTenantId(matched.getTenantId());
        return resolve(node);
    }
    public boolean companyEndpoint(String host, int port) {
        return properties.getAssets()!=null && properties.getAssets().stream().anyMatch(asset -> asset!=null && asset.isCompanyPool()
            && APPROVED_HOSTS.contains(asset.getHost()) && asset.getHost().equals(host) && asset.getPort()==port);
    }

    public Target resolve(ComputeNode node) throws IOException {
        if (!APPROVED_HOSTS.contains(node.getHost()))
            return new Target(PublicDatasetDownloader.publicAddresses(node.getHost()), node.getHostKey(), null);
        ComputeTrustedSelfAssetsProperties.Asset asset = requireAsset(node);
        byte[] expected = key(asset.getHost(), asset.getHostKey());
        if (node.getHostKey() != null && !node.getHostKey().isEmpty()
            && !MessageDigest.isEqual(expected, key(node.getHost(), node.getHostKey())))
            throw new IOException("已登记的主机身份与受信资产不一致，未连接或替换指纹");
        byte last = (byte) ("192.168.88.173".equals(node.getHost()) ? 173 : 99);
        InetAddress address = InetAddress.getByAddress(new byte[]{(byte) 192, (byte) 168, 88, last});
        try { return new Target(new InetAddress[]{address}, asset.getHostKey(), new HostKey(node.getHost(), expected).getType()); }
        catch (Exception error) { throw new IOException("受信资产主机公钥无法解析", error); }
    }

    public ComputeTrustedSelfAssetsProperties.Asset requireAsset(ComputeNode node) throws IOException {
        List<ComputeTrustedSelfAssetsProperties.Asset> assets = properties.getAssets();
        Set<String> seen = new HashSet<>();
        ComputeTrustedSelfAssetsProperties.Asset matched = null;
        if (assets != null) for (ComputeTrustedSelfAssetsProperties.Asset asset : assets) {
            if (asset == null || !APPROVED_HOSTS.contains(asset.getHost()) || asset.getPort() != 22
                || blank(asset.getTenantId()) || blank(asset.getUsername()) || blank(asset.getAssetId())
                || !asset.getUsername().matches("[a-zA-Z_][a-zA-Z0-9_-]{0,63}")
                || !asset.getAssetId().matches("[A-Za-z0-9_.:-]{1,128}") || !seen.add(asset.getHost()))
                throw new IOException("受信自有资产配置无效；仅允许两台已批准主机的完整唯一身份");
            key(asset.getHost(), asset.getHostKey());
            if (asset.getHost().equals(node.getHost())) matched = asset;
        }
        if (matched == null || !"self".equals(node.getProvider()) || node.getPort() != 22
            || !matched.getTenantId().equals(node.getTenantId()) || !matched.getUsername().equals(node.getUsername()))
            throw new IOException("此节点未匹配部署管理员批准的自有资产身份");
        return matched;
    }

    private static byte[] key(String host, String encoded) throws IOException {
        if (blank(encoded) || encoded.length() > 16384) throw new IOException("受信资产缺少已核验的主机公钥");
        try {
            byte[] value = Base64.getDecoder().decode(encoded);
            HostKey key = new HostKey(host, value);
            if (!MODERN_HOST_KEYS.contains(key.getType())) throw new IOException("受信资产主机公钥算法未支持，不能降低主机验证");
            ByteBuffer wire = ByteBuffer.wrap(value);
            String type = new String(field(wire), StandardCharsets.US_ASCII);
            if (!key.getType().equals(type)) throw new IOException("主机公钥类型不匹配");
            if ("ssh-ed25519".equals(type)) {
                if (field(wire).length != 32) throw new IOException("ED25519主机公钥长度无效");
            } else {
                String curve = new String(field(wire), StandardCharsets.US_ASCII);
                byte[] point = field(wire);
                int size = "nistp256".equals(curve) ? 65 : "nistp384".equals(curve) ? 97 : "nistp521".equals(curve) ? 133 : -1;
                if (!type.equals("ecdsa-sha2-" + curve) || point.length != size || point[0] != 4)
                    throw new IOException("ECDSA主机公钥格式无效");
            }
            if (wire.hasRemaining()) throw new IOException("主机公钥包含额外数据");
            return value;
        } catch (IOException error) { throw error; }
        catch (Exception error) { throw new IOException("受信资产主机公钥无效", error); }
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private static byte[] field(ByteBuffer wire) throws IOException {
        if (wire.remaining() < 4) throw new IOException("主机公钥内容不完整");
        int length = wire.getInt();
        if (length < 0 || length > wire.remaining()) throw new IOException("主机公钥字段长度无效");
        byte[] value = new byte[length]; wire.get(value); return value;
    }

    public static final class Target {
        private final InetAddress[] addresses;
        private final String hostKey;
        private final String requiredAlgorithm;
        Target(InetAddress[] addresses, String hostKey, String requiredAlgorithm) {
            this.addresses = addresses.clone(); this.hostKey = hostKey; this.requiredAlgorithm = requiredAlgorithm;
        }
        public InetAddress[] addresses() { return addresses.clone(); }
        public String hostKey() { return hostKey; }
        public String requiredAlgorithm() { return requiredAlgorithm; }
    }
}

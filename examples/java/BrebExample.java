package app.bloque.examples;

import app.bloque.sdk.BloqueSDK;
import app.bloque.sdk.UserSession;
import app.bloque.sdk.accounts.ActivateBrebKeyParams;
import app.bloque.sdk.accounts.BrebKeyAccount;
import app.bloque.sdk.accounts.BrebKeyType;
import app.bloque.sdk.accounts.BrebOperationResult;
import app.bloque.sdk.accounts.BrebResolvedKey;
import app.bloque.sdk.accounts.CreateBrebKeyParams;
import app.bloque.sdk.accounts.DeleteBrebKeyParams;
import app.bloque.sdk.accounts.DeleteBrebKeyResult;
import app.bloque.sdk.accounts.ResolveBrebKeyParams;
import app.bloque.sdk.accounts.SuspendBrebKeyParams;
import app.bloque.sdk.accounts.SuspendBrebKeyResult;
import app.bloque.sdk.accounts.ActivateBrebKeyResult;
import app.bloque.sdk.core.Mode;

import java.util.HashMap;
import java.util.Map;

/**
 * Java example: BRE-B key operations
 *
 * This example demonstrates how to create, resolve, suspend, activate,
 * and delete BRE-B keys using the Bloque SDK in Java.
 */
public class BrebExample {

    public static void main(String[] args) {
        BloqueSDK bloque = BloqueSDK.builder()
            .origin("{your-origin-here}")
            .apiKey("{your-api-key-here}")
            .mode(Mode.SANDBOX)
            .build();

        UserSession session = bloque.connect("nestor");

        System.out.println("=== Example 1: Create BRE-B Key ===");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "example");
        metadata.put("channel", "sdk-java");

        BrebOperationResult<BrebKeyAccount> created = session.getAccounts()
            .getBreb()
            .createKey(new CreateBrebKeyParams(
                BrebKeyType.PHONE,
                "3123185778",
                "Pepito Silva",
                "ledger-account-breb-001",
                null,
                metadata
            ));

        System.out.println("Create error: " +
            (created.getError() != null ? created.getError().getMessage() : "null"));
        System.out.println("Account URN: " +
            (created.getData() != null ? created.getData().getUrn() : "null"));
        System.out.println("Remote Key ID: " +
            (created.getData() != null ? created.getData().getRemoteKeyId() : "null"));
        System.out.println("Status: " +
            (created.getData() != null ? created.getData().getStatus() : "null"));

        System.out.println("\n=== Example 2: Resolve BRE-B Key ===");

        BrebOperationResult<BrebResolvedKey> resolved = session.getAccounts()
            .getBreb()
            .resolveKey(new ResolveBrebKeyParams(BrebKeyType.PHONE, "3123185778"));

        System.out.println("Resolve error: " +
            (resolved.getError() != null ? resolved.getError().getMessage() : "null"));
        System.out.println("Resolution ID: " +
            (resolved.getData() != null ? resolved.getData().getResolutionId() : "null"));
        System.out.println("Owner name: " +
            (resolved.getData() != null && resolved.getData().getOwner() != null
                ? resolved.getData().getOwner().getName()
                : "null"));
        System.out.println("Receptor node: " +
            (resolved.getData() != null ? resolved.getData().getReceptorNode() : "null"));

        if (created.getData() != null) {
            String accountUrn = created.getData().getUrn();

            System.out.println("\n=== Example 3: Suspend BRE-B Key ===");

            BrebOperationResult<SuspendBrebKeyResult> suspended = session.getAccounts()
                .getBreb()
                .suspendKey(new SuspendBrebKeyParams(accountUrn));

            System.out.println("Suspend error: " +
                (suspended.getError() != null ? suspended.getError().getMessage() : "null"));
            System.out.println("Suspend status: " +
                (suspended.getData() != null ? suspended.getData().getStatus() : "null"));
            System.out.println("Upstream key status: " +
                (suspended.getData() != null ? suspended.getData().getKeyStatus() : "null"));

            System.out.println("\n=== Example 4: Activate BRE-B Key ===");

            BrebOperationResult<ActivateBrebKeyResult> activated = session.getAccounts()
                .getBreb()
                .activateKey(new ActivateBrebKeyParams(accountUrn));

            System.out.println("Activate error: " +
                (activated.getError() != null ? activated.getError().getMessage() : "null"));
            System.out.println("Activate status: " +
                (activated.getData() != null ? activated.getData().getStatus() : "null"));
            System.out.println("Upstream key status: " +
                (activated.getData() != null ? activated.getData().getKeyStatus() : "null"));

            System.out.println("\n=== Example 5: Delete BRE-B Key ===");

            BrebOperationResult<DeleteBrebKeyResult> deleted = session.getAccounts()
                .getBreb()
                .deleteKey(new DeleteBrebKeyParams(accountUrn));

            System.out.println("Delete error: " +
                (deleted.getError() != null ? deleted.getError().getMessage() : "null"));
            System.out.println("Deleted: " +
                (deleted.getData() != null ? deleted.getData().getDeleted() : "null"));
            System.out.println("Delete status: " +
                (deleted.getData() != null ? deleted.getData().getStatus() : "null"));
        }

        System.out.println("\n=== All BRE-B Examples Completed ===");
    }
}
